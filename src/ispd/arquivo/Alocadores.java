package ispd.arquivo;

import ispd.alocacaoVM.ManipularArquivosAlloc;

import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO: Perhaps this class could be fully static, loading everything only once
//  when needed.

// TODO: Runtime compilation may not be the best way to do this

/**
 * Manages storing, retrieving and compiling allocation policies
 */
public class Alocadores implements ManipularArquivosAlloc {

    private static final String NO_POLICY = "---";
    /**
     * Allocation policies available by default
     */
    public static final String[] ALOCACAO = { Alocadores.NO_POLICY,
            "RoundRobin", "FirstFit", "FirstFitDecreasing", "Volume" };

    // TODO: This directory should be in the configuration file
    private static final String DIRECTORY_PATH = "ispd/externo/cloudAlloc";
    private static final File DIRECTORY = new File(Alocadores.DIRECTORY_PATH);
    private final ArrayList<String> policies = new ArrayList<>(0);
    private final List<String> addedPolicies = new ArrayList<>(0);
    private final List<String> removedPolicies = new ArrayList<>(0);

    public Alocadores() {
        if (Alocadores.DIRECTORY.exists()) {
            this.findDotClassAllocators();
        } else {

            /// TODO: Find better way to do this
            try {
                Alocadores.createDirectory(Alocadores.DIRECTORY);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            if (Objects.requireNonNull(this.getClass().getResource(
                    "Alocadores.class")).toString().startsWith("jar:")) {
                Alocadores.executeFromJar();
            }
        }
    }

    private void findDotClassAllocators() {
        final FilenameFilter filter = (b, name) -> name.endsWith(".class");
        final var dotClassFiles =
                Objects.requireNonNull(Alocadores.DIRECTORY.list(filter));

        Arrays.stream(dotClassFiles)
                .map(Alocadores::removeDotClassSuffix)
                .forEach(this.policies::add);
    }

    private static void createDirectory(final File dir) throws IOException {
        if (!dir.mkdirs()) {
            throw new IOException("Failed to create directory " + dir);
        }
    }

    private static void executeFromJar() {
        final var jar = new File(
                System.getProperty("java.class.path"));

        try {
            Alocadores.extractDirFromJar("alocacaoVM", jar);
            Alocadores.extractDirFromJar("motor", jar);
        } catch (final IOException ex) {
            Logger.getLogger(Alocadores.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    private static String removeDotClassSuffix(final String s) {
        return s.substring(0, s.length() - ".class".length());
    }

    /**
     * Extracts given dir from jar file given by file.
     *
     * @param dir  Directory name to be extracted
     * @param file Jar file from which to extracted the directory
     */
    private static void extractDirFromJar(final String dir, final File file) throws IOException {
        try (final var jar = new JarFile(file)) {
            for (final var entry : new JarEntryIterable(jar)) {
                if (entry.getName().contains(dir)) {
                    Alocadores.processZipEntry(entry, jar);
                }
            }
        }
    }

    private static void processZipEntry(
            final ZipEntry entry, final ZipFile zip) throws IOException {
        final var file = new File(entry.getName());

        if (entry.isDirectory() && !file.exists()) {
            Alocadores.createDirectory(file);
            return;
        }

        if (!file.getParentFile().exists()) {
            Alocadores.createDirectory(file.getParentFile());
        }

        // TODO: Discuss possibility of Files.copy()
        try (final var is = zip.getInputStream(entry);
             final var os = new FileOutputStream(file)) {
            is.transferTo(os);
        }
    }

    /**
     * @return Basic template for writing an allocation policy's source code
     */
    public static String getAlocadorJava(final String policyName) {
        return """
                package ispd.externo;
                import ispd.alocacaoVM.Alocacao;
                import ispd.motor.filas.servidores.implementacao.CS_VirtualMac;
                import ispd.motor.filas.servidores.CS_Processamento;
                import ispd.motor.filas.servidores.CentroServico;
                import ispd.motor.filas.servidores.implementacao.CS_MaquinaCloud;
                import ispd.motor.filas.servidores.implementacao.CS_VMM;
                import java.util.List;

                public abstract class %s extends Alocacao{

                    @Override
                    public void iniciar() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public CS_VirtualMac escalonarVM() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public CS_Processamento escalonarRecurso() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public List<CentroServico> escalonarRota(CentroServico destino) {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                    @Override
                    public void escalonar() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }
                    @Override
                    public void migrarVM() {
                        throw new UnsupportedOperationException("Not supported yet.");
                    }

                }""".formatted(policyName);
    }

    /**
     * Lists all available allocation policies.
     *
     * @return {@code ArrayList} with all allocation policies' names
     */
    @Override
    public ArrayList<String> listar() {
        return this.policies;
    }

    /**
     * @return Directory in which allocation policies sources and compiled
     * classes are saved
     */
    @Override
    public File getDiretorio() {
        return Alocadores.DIRECTORY;
    }

    /**
     * Writes the contents of String codigo into the source file of the policy
     * given by nome.
     *
     * @param nome   Name of the policy which source file will be written to
     * @param codigo Contents to be written in the file
     * @return {@code true} if writing was successful
     */
    @Override
    public boolean escrever(final String nome, final String codigo) {
        try (final var fw = new FileWriter(
                new File(Alocadores.DIRECTORY, nome + ".java"),
                StandardCharsets.UTF_8
        )) {
            fw.write(codigo);
            return true;
        } catch (final IOException ex) {
            Logger.getLogger(Alocadores.class.getName())
                    .log(Level.SEVERE, null, ex);
            return false;
        }
    }

    /**
     * Attempts to compile the source file of the policy with given name,
     * returning the contents of stderr, if any, otherwise {@code null}.
     *
     * @param nome Name of the allocation policy to be compiled
     * @return A string with errors, if any, otherwise {@code null}
     */
    @Override
    public String compilar(final String nome) {
        final var target = new File(Alocadores.DIRECTORY, nome + ".java");
        final var err = Alocadores.compile(target);

        // TODO: What?
        try {
            Thread.sleep(1000);
        } catch (final InterruptedException ex) {
            Logger.getLogger(Alocadores.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        // Check if compilation worked, looking for a .class file
        if (new File(Alocadores.DIRECTORY, nome + ".class").exists()) {
            this.addPolicy(nome);
        }

        return err.isEmpty() ? null : err;
    }

    private static String compile(final File target) {
        final var compiler = ToolProvider.getSystemJavaCompiler();

        if (compiler != null) {
            final var err = new ByteArrayOutputStream();
            compiler.run(null, null, err, target.getPath());
            return err.toString();
        } else {
            try {
                return Alocadores.compileManually(target);
            } catch (final IOException ex) {
                Logger.getLogger(Alocadores.class.getName())
                        .log(Level.SEVERE, null, ex);
                // TODO: More useful error messages
                return "Não foi possível compilar";
            }
        }
    }

    /**
     * Adds policy to the inner list of policies
     */
    private void addPolicy(final String policyName) {
        if (this.policies.contains(policyName)) {
            return;
        }

        this.policies.add(policyName);
        this.addedPolicies.add(policyName);
    }

    private static String compileManually(final File target) throws IOException {
        final var proc = Runtime.getRuntime().exec("javac " + target.getPath());

        // TODO: Extract commonalities with method ler()
        try (final var err = new BufferedReader(new InputStreamReader(
                proc.getErrorStream(), StandardCharsets.UTF_8))
        ) {
            return err.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Reads the source file from the policy alocador and returns a string
     * with the file contents.
     *
     * @param alocador Name of the policy which source file will be read
     * @return String contents of the file.
     */
    @Override
    public String ler(final String alocador) {
        try (final var br = new BufferedReader(
                new FileReader(
                        new File(Alocadores.DIRECTORY, alocador + ".java"),
                        StandardCharsets.UTF_8)
        )) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (final IOException ex) {
            Logger.getLogger(Alocadores.class.getName())
                    .log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * Attempts to remove .java and .class files with the name in "alocador"
     * and, if successful, removes the policy from the inner list.
     *
     * @param alocador Name of the policy which files be removed
     * @return {@code true} if removal is successful
     */
    @Override
    public boolean remover(final String alocador) {
        final var classFile = new File(
                Alocadores.DIRECTORY, alocador + ".class");

        final File javaFile = new File(
                Alocadores.DIRECTORY, alocador + ".java");

        boolean deleted = false;

        if (classFile.exists()) {
            if (classFile.delete()) {
                this.removePolicy(alocador);
                deleted = true;
            }
        }

        if (javaFile.exists()) {
            if (javaFile.delete()) {
                deleted = true;
            }
        }

        return deleted;
    }

    private void removePolicy(final String policyName) {
        if (!this.policies.contains(policyName)) {
            return;
        }

        this.policies.remove(policyName);
        this.removedPolicies.add(policyName);
    }

    /**
     * Adds allocation policy coded in file {@code arquivo} to the configured
     * directory, compiles it, and adds it to the list of allocation policies.
     *
     * @param arquivo Java source file containing the allocation policy
     * @return {@code true} if import ocurred succesfully and {@code false}
     * otherwise
     */
    @Override
    public boolean importarAlocadoresJava(final File arquivo) {
        // TODO: Merge this and method compilar() into one

        final var target = new File(Alocadores.DIRECTORY, arquivo.getName());
        Alocadores.copyFile(target, arquivo);

        final var err = Alocadores.compile(target);

        if (!err.isEmpty()) {
            return false;
        }

        final var nome = arquivo.getName()
                .substring(0, arquivo.getName().length() - ".java".length());

        if (!new File(Alocadores.DIRECTORY, nome + ".class").exists()) {
            return false;
        }

        this.addPolicy(nome);

        return true;
    }

    // TODO: Check if Files.copy works
    private static void copyFile(final File dest, final File src) {
        if (dest.getPath().equals(src.getPath())) {
            return;
        }

        try (final var srcFs = new FileInputStream(src);
             final var destFs = new FileOutputStream(dest)) {
            srcFs.transferTo(destFs);
        } catch (final IOException ex) {
            Logger.getLogger(Alocadores.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public List listarAdicionados() {
        return this.addedPolicies;
    }

    @Override
    public List listarRemovidos() {
        return this.removedPolicies;
    }

    private record JarEntryIterable(JarFile jar) implements Iterable<JarEntry> {
        @Override
        public Iterator<JarEntry> iterator() {
            return this.jar.entries().asIterator();
        }
    }
}