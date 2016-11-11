
/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LSTORE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Goal that performs tail-rec optimizations
 * @threadSafe false
 * @goal tail-rec-optimize
 * @phase process-classes
 */
public class MyMojo extends AbstractMojo {
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    public void execute() throws MojoExecutionException {
        File f = outputDirectory;

        if (!f.exists()) {
            f.mkdirs();
        }

        List<File> clzes = getAllClassFiles(new File(f.getAbsoluteFile()
                + File.separator + "classes"));
        try {
            for (File file : clzes) {
                optimize(file);
            }
        } catch (Exception e) {
            MyMojo.<RuntimeException> convert(e);
        }

    }

    private static <T extends Throwable> void convert(Throwable t) throws T {
        throw (T) t;
    }

    private List<File> getAllClassFiles(File dir) {
        if (dir.isDirectory()) {
            List<File> results = new ArrayList<File>();
            for (File f : dir.listFiles()) {
                results.addAll(getAllClassFiles(f));
            }
            return results;
        }

        if (dir.getName().endsWith(".class")) {
            return Collections.singletonList(dir);
        } else {
            return Collections.emptyList();
        }

    }

    private void optimize(File clz) throws Exception {
        InputStream is = new FileInputStream(clz);
        ClassReader cr = new ClassReader(is);
        is.close();
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        List methods = cn.methods;
        for (int i = 0; i < methods.size(); ++i) {
            MethodNode method = (MethodNode) methods.get(i);

            if (!isStaticllyBound(method)) {
                continue;
            }

            InsnList instructions = method.instructions;
            AbstractInsnNode last = instructions.getLast();

            while (last != null && last.getType() == AbstractInsnNode.LABEL) {
                last = last.getPrevious();
            }

            if (last == null
                    || !isReturnInstruction(last)
                    || last.getPrevious().getType() != AbstractInsnNode.METHOD_INSN) {
                continue;
            }

            MethodInsnNode methodInv = (MethodInsnNode) last.getPrevious();

            if (!isRecursionCall(cn, method, methodInv)) {
                continue;
            }

            getLog().info("TailRec Optimizaing: " + method.name);

            // get arguments and types
            String methodDesc = method.desc;

            String argsDesc = methodDesc.substring(methodDesc.indexOf('(') + 1,
                    methodDesc.indexOf(')'));
            //System.out.println(argsDesc);

            // work with Type.getArgumentTypes
            List<AbstractInsnNode> listInstNodes = new LinkedList<AbstractInsnNode>();
            for (int j = argsDesc.length()-1; j >= 0 ; j--) {
                char c = argsDesc.charAt(j);
                switch (c) {
                    case 'I':
                        listInstNodes.add(new VarInsnNode(ISTORE, argsDesc.length()
                                - j));
                        break;
                    case 'Z':
                        listInstNodes.add(new VarInsnNode(ISTORE, argsDesc.length()
                                - j));
                        break;
                    case 'C':
                        listInstNodes.add(new VarInsnNode(ISTORE, argsDesc.length()
                                - j));
                        break;
                    case 'B':
                        listInstNodes.add(new VarInsnNode(ISTORE, argsDesc.length()
                                - j));
                        break;
                    case 'S':
                        listInstNodes.add(new VarInsnNode(ISTORE, argsDesc.length()
                                - j));
                        break;
                    case 'F':
                        listInstNodes.add(new VarInsnNode(FSTORE, argsDesc.length()
                                - j));
                        break;
                    case 'J':
                        listInstNodes.add(new VarInsnNode(LSTORE, argsDesc.length()
                                - j));
                        break;
                    case 'D':
                        listInstNodes.add(new VarInsnNode(DSTORE, argsDesc.length()
                                - j));
                        break;
                    case '[':
                        // TODO:
                    case 'L':
                        // TODO:
                    default:
                        System.out.println("NOT TREATING: " + c);
                }

            }

            // remove the last aload_0 of the recursion
            AbstractInsnNode pnt = last;
            while (pnt != null && pnt.getOpcode() != 42
                    && !(pnt.getOpcode() == 25 && ((VarInsnNode) pnt).var == 0)) {
                pnt = pnt.getPrevious();
            }
            method.instructions.remove(pnt);

            Collections.reverse(listInstNodes);
            for (AbstractInsnNode abstractInsnNode : listInstNodes) {
                method.instructions.insertBefore(last.getPrevious(),
                        abstractInsnNode);
            }
            // place instead of return //goto

            LabelNode startOfMethodLabel = new LabelNode(new Label());
            method.instructions.insertBefore(method.instructions.getFirst(),
                    startOfMethodLabel);
            JumpInsnNode gotoInst = new JumpInsnNode(GOTO, startOfMethodLabel);
            method.instructions.set(last.getPrevious(), gotoInst);
            method.instructions.remove(last);

            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            FileOutputStream fos = new FileOutputStream(clz);
            fos.write(cw.toByteArray());
            fos.close();
        }
    }

    private static boolean isRecursionCall(ClassNode cn, MethodNode method,
                                           MethodInsnNode methodInv) {
        return methodInv.owner.equals(cn.name)
                && methodInv.name.equals(method.name)
                && methodInv.desc.equals(method.desc);
    }

    private static boolean isReturnInstruction(AbstractInsnNode inst) {
        int opcode = inst.getOpcode();
        return opcode <= 177 && opcode >= 172;
    }

    private static boolean isStaticllyBound(MethodNode method) {
        if ((method.access & (ACC_FINAL | ACC_PRIVATE | ACC_STATIC)) > 0) {
            return true;
        }
        return false;
    }

}