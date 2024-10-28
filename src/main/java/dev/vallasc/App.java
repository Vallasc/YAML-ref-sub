package dev.vallasc;

import org.jsonschema2pojo.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class App {
    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("Hello World!");

        URL source = new URI("https://raw.githubusercontent.com/Vallasc/YAML-ref-sub/refs/heads/main/testfiles/main/main.yaml").toURL();
        String outDir = "./target/generated/jsonschema";

        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return false;
            }

            @Override
            public SourceType getSourceType() {
                return SourceType.YAMLSCHEMA;
            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return false;
            }

            @Override
            public File getTargetDirectory() {
                return new File(outDir);
            }

            @Override
            public Iterator<URL> getSource() {
                return Collections.singletonList(source).iterator();
            }
        };

        generateSources(config);
        System.out.println(replaceRelativeRefs(source));
    }

    public static void generateSources(GenerationConfig config) throws IOException {
        Jsonschema2Pojo.generate(config, new NoopRuleLogger());
    }

    public static String replaceRelativeRefs(URL yamlFileUrl) throws IOException, URISyntaxException {
        // Carica e processa il file YAML
        InputStream inputStream = yamlFileUrl.openStream();
        Yaml yaml = new Yaml();
        Map<String, Object> yamlData = yaml.load(inputStream);

        // Processa i riferimenti $ref
        replaceRefs(yamlData, yamlFileUrl);

        // Stampa il YAML modificato
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml outputYaml = new Yaml(options);
        return outputYaml.dump(yamlData);
    }

    @SuppressWarnings("unchecked")
    private static void replaceRefs(Map<String, Object> node, URL mainYamlUrl) throws MalformedURLException, URISyntaxException {
        if (node == null) return;

        for (Map.Entry<String, Object> entry : node.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<String, Object> childNode = (Map<String, Object>) entry.getValue();

                // Controlla e sostituisce i riferimenti $ref
                if (childNode.containsKey("$ref")) {
                    String refPath = (String) childNode.get("$ref");

                    // Se il riferimento è relativo, convertilo a uno statico
                    URL resolvedUrl = new URL(mainYamlUrl, refPath); // Risolve il percorso relativo
                    childNode.put("$ref", resolvedUrl.toString());
                }
                // Ricorsivamente processa i nodi figli
                replaceRefs(childNode, mainYamlUrl);
            }
        }
    }
}
