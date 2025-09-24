import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class debug_test {
    public static void main(String[] args) throws IOException {
        Path realCrdDirectory = Paths.get("src/main/resources/crds");
        Path outputDirectory = Files.createTempDirectory("test-output");
        
        System.out.println("Real CRD directory: " + realCrdDirectory);
        System.out.println("Output directory: " + outputDirectory);
        
        var processor = new org.waveywaves.jenkins.plugins.tekton.generator.TektonCrdToJavaProcessor();
        processor.processDirectory(realCrdDirectory, outputDirectory, "org.waveywaves.jenkins.plugins.tekton.generated", true);
        
        System.out.println("\nGenerated files:");
        Files.walk(outputDirectory)
            .filter(Files::isRegularFile)
            .filter(p -> p.toString().contains("Create"))
            .forEach(System.out::println);
    }
}
