package ch.zimmj.fop.exmaple;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.xml.sax.SAXException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component (
        immediate = true
)
public class fopPdfCreater {

    private static final String PATH_TO_FOP = "/META-INF/resources/fop/template.xslt";
    private static final String PATH_TO_DATA = "/META-INF/resources/data/data.xml";

    @Activate
    public void activate() {
        try {
            ByteArrayOutputStream pdf = getPdfFromJson();
            String workingDir = System.getProperty("user.dir");
            System.out.println("The PDF will be generated in the working directory: "
                    + workingDir + "/../../../"
                    + " \n Which should be tha base directory: Liferay-Thridparty"
            );
            try(OutputStream outputStream = new FileOutputStream(workingDir + "/../../../random.pdf")) {
                pdf.writeTo(outputStream);
            }catch (Exception e) {
                System.err.println(e);
            }

        }catch (Exception e) {
            System.err.println(e);
        }
    }

    public ByteArrayOutputStream getPdfFromJson() throws SAXException, TransformerException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream xmlData = classLoader.getResourceAsStream(PATH_TO_DATA);
        InputStream xslEntry = classLoader.getResourceAsStream(PATH_TO_FOP);

        File fileXml = createFile("data", "xml", xmlData);
        File fileXsl = createFile("structure", "xsl", xslEntry);

        TransformerFactory factory = TransformerFactory.newInstance();
        FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, out);

        Source src = new StreamSource(fileXml);
        Source source = new StreamSource(fileXsl);
        Transformer transformer = factory.newTransformer(source);

        Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);


        fileXml.delete();
        fileXsl.delete();

        return out;
    }

    private File createFile(String fileName, String fileExtension, InputStream fileInputStream) throws IOException {
        File file = File.createTempFile(fileName, "." + fileExtension);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = fileInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Could not create " + fileExtension + " XSL file");
        }
        return file;
    }
}
