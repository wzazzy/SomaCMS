package fi.essentia.simplecms.controllers;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import fi.essentia.simplecms.dao.DataDao;
import fi.essentia.simplecms.json.SearchResult;
import fi.essentia.simplecms.tree.DocumentManager;
import fi.essentia.simplecms.tree.TreeDocument;
import fi.essentia.simplecms.util.ArchiveHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;

/**
 * All the administration logic for the
 */
@Controller
@RequestMapping(value="/admin/")
@Secured(value = "ROLE_ADMIN")
public class AdminController {
    public static final String SUCCESS = "{\"success\":true}";

    @Autowired private DocumentManager documentManager;
    @Autowired private DataDao dataDao;
    @Autowired private ArchiveHelper archiveHelper;

    @RequestMapping(method=RequestMethod.GET)
    public String admin() {
        return "redirect:document/0";
    }

    @RequestMapping(value="/document/{id}", method=RequestMethod.GET)
    public String showFolder(@PathVariable Long id, Model model) {
        TreeDocument document = documentManager.documentById(id);
        if (document == null) {
            throw new ResourceNotFoundException();
        }
        model.addAttribute("document", document);
        if (document.isFolder()) {
            return "folder";
        } else if (document.isImage()) {
            return "image";
        } else if (document.isText()) {
            byte[] bytes = dataDao.loadData(document.getId());
            model.addAttribute("documentText", new String(bytes));
            return "text";
        } else {
            throw new ResourceNotFoundException();
        }
    }

    @RequestMapping(value="/document/{parentId}/folders", method=RequestMethod.POST)
    public @ResponseBody String folders(@PathVariable Long parentId, @RequestParam("name") String name) {
        documentManager.createFolder(parentId, name);
        return SUCCESS;
    }

    @RequestMapping(value="/document/{parentId}/files", method=RequestMethod.POST)
    public @ResponseBody String uploadFile(@PathVariable Long parentId, @RequestParam(value="qqfile", required=true) MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType.equals("application/zip")) {
            byte[] bytes = file.getBytes();
            archiveHelper.storeDocuments(parentId, bytes);
        } else {
            documentManager.storeDocument(parentId, file.getOriginalFilename(), file.getBytes());
        }
        return SUCCESS;
    }

    @RequestMapping(value="/document/{documentId}", method=RequestMethod.PUT)
    public @ResponseBody String saveTextDocument(@PathVariable Long documentId, @RequestBody String contents) {
        dataDao.updateData(documentId, contents.getBytes());
        return SUCCESS;
    }

    @RequestMapping(value="/document/{documentId}", method=RequestMethod.DELETE)
    public @ResponseBody String delete(@PathVariable Long documentId) {
        documentManager.deleteDocument(documentId);
        return SUCCESS;
    }

    @RequestMapping(value= "/search/", method = RequestMethod.GET)
    public @ResponseBody Collection<SearchResult> listDocuments(@RequestParam(value = "query") String query) {
        Collection<TreeDocument> treeDocuments = documentManager.documentsByPath(query);
        return Collections2.transform(treeDocuments, new Function<TreeDocument, SearchResult>() {
            @Override
            public SearchResult apply(TreeDocument treeDocument) {
                return new SearchResult(treeDocument.getId(), treeDocument.getPath());
            }
        });
    }
}