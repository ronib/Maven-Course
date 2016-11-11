import java.io.File;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Goal which touches a timestamp file.
 *
 * @goal warn
 *
 * @phase process-sources
 */
/**
 * Created by Shironi on 11-Nov-16.
 */
public class MyMojo extends AbstractMojo
{
    /**
     * Location of the file.
     * @parameter property="project.build.directory"
     * @required
     */
    private File outputDirectory;

    /**
     * @parameter property="project"
     * @required
     */
    private MavenProject mavenProject;

    public void execute()
            throws MojoExecutionException
    {
        Map<String, String> gaVersion = new HashMap<String, String>();

        for (MavenProject mp : mavenProject.getCollectedProjects()){
            for (Dependency dependency : mp.getDependencies()){
                String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
                if (gaVersion.containsKey(key)) {
                    if (!gaVersion.get(key).equals(dependency.getVersion())) {
                        getLog().warn("[WARNING] !!! duplicate version! for " + key);
                        getLog().warn("[WARNING] !!! duplicate version! already have: " + gaVersion.get(key) + " new: " + dependency.getVersion());
                    }
                }
                else
                    gaVersion.put(key, dependency.getVersion());
            }
        }


//        List<MavenProject> allProjects = project.getCollectedProjects();
//
//        Stream<Dependency> allDependencies = allProjects.stream().flatMap(
//                proj -> proj.getDependencies().stream());
//
//        Map<String, Set<Dependency>> result = allDependencies.collect(Collectors.groupingBy(
//                this::groupAndArtifact, Collectors.toSet()));
//
//
//        for (Map.Entry<String, Set<Dependency>> entry : result.entrySet()) {
//            if (entry.getValue().stream().map(Dependency::getVersion).collect(Collectors.toSet()).size() > 1) {
//                getLog().warn("Multiple versions found for: " + entry.getKey());
//            }
//        }
    }


    private String groupAndArtifact(Dependency d) {
        return d.getGroupId() + "|" + d.getArtifactId();
    }







}