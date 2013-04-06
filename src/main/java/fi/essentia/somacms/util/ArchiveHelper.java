package fi.essentia.somacms.util;

import fi.essentia.somacms.tree.DocumentManager;
import fi.essentia.somacms.tree.TreeDocument;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.activation.MimetypesFileTypeMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stores multiple documents to the given folder by reading their contents from a ZIP archive
 */
@Component
public class ArchiveHelper {
    @Autowired private DocumentManager documentManager;

    public void storeDocuments(Long rootId, byte[] bytes) throws IOException {
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes));
        ZipEntry zipEntry = in.getNextEntry();
        while(zipEntry!=null){
            File file = new File(zipEntry.getName());
            String parentPath = file.getParent();
            TreeDocument parent;
            if (parentPath == null) {
                parent = documentManager.documentById(rootId);
            } else {
                parent = documentManager.documentFromPath(parentPath);
            }
            if (parent == null) {
                throw new NullPointerException("Failed to find the parent folder for " + zipEntry.getName());
            }

            if (zipEntry.isDirectory()) {
                if (parent.childByName(file.getName()) == null) {
                    documentManager.createFolder(parent.getId(), file.getName());
                }
            } else {
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                IOUtils.copy(in, out);
                out.close();
                documentManager.storeDocument(parent.getId(), file.getName(), out.toByteArray());
            }
            zipEntry = in.getNextEntry();
        }
        in.closeEntry();
        in.close();
    }
}