package com.change_vision.astah.exporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.confluence.pages.Attachment;
import com.change_vision.astah.file.AstahBaseDirectory;
import com.change_vision.astah.file.ExportBaseDirectory;
import com.change_vision.astah.file.ExportRootDirectory;
import com.change_vision.astah.util.Util;

public class DiagramExportRunnable implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DiagramExportRunnable.class);

    private File tmpRoot = new File(System.getProperty("java.io.tmpdir"), "astah-temp");

    private final AstahBaseDirectory astahBase;

    private final Util util = new Util();

    private final ObjectMapper mapper = new ObjectMapper();

    private final Attachment attachment;

    private ExportSetting setting = new ExportSetting();

    boolean success = false;

    private ExportBaseDirectory exportBase;

    public DiagramExportRunnable(final Attachment attachment, final AstahBaseDirectory astahBase,
            final ExportBaseDirectory exportBase) {
        if (attachment == null) {
            throw new IllegalArgumentException("attachment is null.");
        }
        this.attachment = attachment;
        this.astahBase = astahBase;
        this.exportBase = exportBase;
    }

    @Override
    public void run() {
        if (astahBase.isValid() == false) {
            logger.error("Illegal state of exporting astah diagrams environment.");
            return;
        }
        
        File file = storeToTempDir(attachment);
        logger.info("file : {}", file.getAbsolutePath());

        File javaCommand = getJavaCommand();
        if (!javaCommand.exists()) {
            logger.error("Can't find java command. '{}'",javaCommand.getAbsolutePath());
            return;
        }

        ExportRootDirectory exportRootDirectory = createExportRoot();

        String[] commands = createExportCommand(file, javaCommand, exportRootDirectory);

        startExportProcess(commands);

        final File output = exportRootDirectory.getDirectory();
        if (!output.exists()) {
            logger.error("Can't create output directory. May be permission issue.'{}'",
                    output.getAbsolutePath());
            return;
        }

        File[] outputFiles = output.listFiles();
        if (isCreated(outputFiles)) {
            logger.error("can't export Astah's diagram files.");
            return;
        }

        createDiagramIndexFile(output);
        success = true;
    }

    private boolean isCreated(File[] outputFiles) {
        return outputFiles == null || outputFiles.length == 0;
    }

    private void createDiagramIndexFile(final File output) {
        List<File> pngFiles = new ArrayList<File>();
        traverseFiles(output, pngFiles);
        exportFileIndexFile(output, pngFiles);
        exportDiagarmIndexFile(output, pngFiles);
    }

    private String[] createExportCommand(File file, File javaCommand, ExportRootDirectory exportRoot) {
        List<String> commands = new ArrayList<String>();
        addSettingFromFile(javaCommand, commands);
        commands.add("-Djava.awt.headless=true");
        commands.add("-Dcheck_jvm_version=false");
        commands.add("-cp");
        commands.add(astahBase.getAstahJar(file).getAbsolutePath());
        commands.add("com.change_vision.jude.cmdline.JudeCommandRunner");
        commands.add("-image");
        commands.add( "all");
        commands.add("-resized");
        commands.add("-f");
        commands.add(file.getAbsolutePath());
        commands.add("-t");
        commands.add("png");
        commands.add("-o");
        commands.add(exportRoot.getDirectory().getAbsolutePath());
        logger.info("args: {}", commands);
        return commands.toArray(new String[0]);
    }

    private void addSettingFromFile(File javaCommand, List<String> commands) {
        File settingFile = astahBase.getExportIniFile();
        if (settingFile == null || settingFile.exists() == false) {
            return;
        }
        String[] loadedSettings = setting.load(settingFile);
        commands.add(javaCommand.getAbsolutePath());
        for (String loadedSetting : loadedSettings) {
            commands.add(loadedSetting);
        }
    }

    private void startExportProcess(String[] commands) {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        Process p = null;
        try {
            p = builder.start();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        logProcessOutput(p.getInputStream());
    }

    private void logProcessOutput(InputStream is) {
        BufferedReader reader = null;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            reader = new BufferedReader(inputStreamReader);
            String line;
            while (null != (line = reader.readLine())) {
                logger.info(line);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            try {
                is.close();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private File getJavaCommand() {
        File javaHome = new File(System.getProperty("java.home"));
        File javaBin = new File(javaHome, "bin");
        String javaCommandName = util.getJavaCommandName();
        File javaCommand = new File(javaBin, javaCommandName);
        return javaCommand;
    }

    private ExportRootDirectory createExportRoot() {
        return new ExportRootDirectory(exportBase, attachment);
    }

    private void exportFileIndexFile(File outputRoot, List<File> pngFiles) {
        try {
            String[] filePaths = new String[pngFiles.size()];
            for (int index = 0; index < pngFiles.size(); index++) {
                File pngFile = pngFiles.get(index);
                String relativePath = util.getRelativePath(exportBase.getDirectory().getAbsolutePath(), pngFile);
                filePaths[index] = relativePath;
            }
            File file = new File(outputRoot, "file.json");
            mapper.writeValue(file, filePaths);
        } catch (IOException e) {
            logger.error("error has occurred when making index file.", e);
        }
    }

    private void exportDiagarmIndexFile(File outputRoot, List<File> pngFiles) {
        try {
            String[] fileNames = new String[pngFiles.size()];
            for (int index = 0; index < pngFiles.size(); index++) {
                File pngFile = pngFiles.get(index);
                String filename = util.getFilename(pngFile);
                fileNames[index] = filename;
            }
            File file = new File(outputRoot, "diagram.json");
            mapper.writeValue(file, fileNames);
        } catch (IOException e) {
            logger.error("error has occurred when making index file.", e);
        }
    }

    private void traverseFiles(File outputRoot, List<File> pngFiles) {
        File[] files = outputRoot.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                traverseFiles(file, pngFiles);
            }

            String suffix = util.getFileExtension(file);
            if (suffix != null && suffix.equalsIgnoreCase("png")) {
                logger.info("exported file '{}'", file.getName());
                pngFiles.add(file);
            }
        }
    }

    private File storeToTempDir(Attachment attachment) {
        try {
            long id = attachment.getId();
            Integer attachmentVersion = attachment.getAttachmentVersion();
            File tmpAttachmentRootDir = new File(tmpRoot, String.valueOf(id));
            File tmpDir = new File(tmpAttachmentRootDir, String.valueOf(attachmentVersion));
            tmpDir.mkdirs();
            File tmpFile = new File(tmpDir, attachment.getFileName());
            InputStream inputStream = attachment.getContentsAsStream();
            FileOutputStream outputStream = new FileOutputStream(tmpFile);
            IOUtils.copy(inputStream, outputStream);
            IOUtils.closeQuietly(outputStream);
            tmpFile.deleteOnExit();
            tmpDir.deleteOnExit();
            tmpAttachmentRootDir.deleteOnExit();
            return tmpFile;
        } catch (IOException e) {
            logger.error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    void setTmpRoot(File tmpRoot) {
        this.tmpRoot = tmpRoot;
    }

}
