package solrj;

import static java.lang.String.valueOf;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.xml.sax.SAXException;

import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;

public class PstPoster {
    private HttpSolrServer server = new HttpSolrServer("http://10.1.1.221:8983/solr/mailclean");
    private int count = 0;
    private String file_path = "C:\\Users\\xmartijanicolas\\Desktop\\sally_beck\\sally_beck\\sally_beck_001_1_2_1.pst";

    public static void main(String[] args) {
        PstPoster aPstPoster = new PstPoster();
        aPstPoster.postDoc();
    }

    public void postDoc() {

        PSTFile pstFile = null;
        try {
            pstFile = new PSTFile(file_path);
            postFolder(pstFile.getRootFolder());
            server.commit();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (pstFile != null && pstFile.getFileHandle() != null) {
                try {
                    pstFile.getFileHandle().close();
                } catch (IOException e) {
                    // swallow closing exception
                }
            }
        }

    }

    private void postFolder(PSTFolder pstFolder) throws Exception {
        if (pstFolder.getContentCount() > 0) {
            PSTMessage pstMail = (PSTMessage) pstFolder.getNextChild();
            while (pstMail != null) {
                parserMailItem(pstFolder, pstMail);
                pstMail = (PSTMessage) pstFolder.getNextChild();
                count += 1;
                if (count % 50 == 0)
                    server.commit();
            }
        }

        if (pstFolder.hasSubfolders()) {
            for (PSTFolder pstSubFolder : pstFolder.getSubFolders()) {
                postFolder(pstSubFolder);
            }
        }
    }

    private void parserMailItem(PSTFolder pstFolder, PSTMessage pstMail) throws SAXException, IOException {
        SolrInputDocument doc = new SolrInputDocument();

        doc.addField("filepath", file_path);
        doc.addField("pstFolder", pstFolder.getDisplayName());
        doc.addField("id", pstMail.getInternetMessageId());
        doc.addField("RESOURCE_NAME_KEY", pstMail.getInternetMessageId());
        doc.addField("EMBEDDED_RELATIONSHIP_ID", pstMail.getInternetMessageId());
        doc.addField("IDENTIFIER", pstMail.getInternetMessageId());
        doc.addField("TITLE", pstMail.getSubject());
        doc.addField("MESSAGE_FROM", pstMail.getSenderName());
        doc.addField("CREATOR", pstMail.getSenderName());
        doc.addField("CREATED", pstMail.getCreationTime());
        doc.addField("MODIFIED", pstMail.getLastModificationTime());
        doc.addField("COMMENTS", pstMail.getComment());
        doc.addField("descriptorNodeId", valueOf(pstMail.getDescriptorNodeId()));
        doc.addField("senderEmailAddress", pstMail.getSenderEmailAddress());
        doc.addField("recipients", pstMail.getRecipientsString());
        doc.addField("displayTo", pstMail.getDisplayTo());
        doc.addField("displayCC", pstMail.getDisplayCC());
        doc.addField("displayBCC", pstMail.getDisplayBCC());
        doc.addField("importance", valueOf(pstMail.getImportance()));
        doc.addField("priority", valueOf(pstMail.getPriority()));
        doc.addField("flagged", valueOf(pstMail.isFlagged()));
        doc.addField("body", pstMail.getBody());

        try {
            server.add(doc);
        } catch (Exception e) {
            System.out.println(e.getMessage() + " at " + pstFolder.getDisplayName() + " id  "
                    + pstMail.getInternetMessageId());
        }
    }

}
