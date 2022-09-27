import com.amazon.textract.pdf.ImageType;
import com.amazon.textract.pdf.PDFDocument;
import com.amazon.textract.pdf.TextLine;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.*;
import com.amazonaws.util.IOUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DemoPdfFromLocalImage {

    public void run(String documentName, String outputDocumentName, boolean wordBased, boolean showOverlayedText) throws IOException {

        System.out.println("Generating searchable pdf from: " + documentName);
        System.out.println("Show Overlayed Text: " + showOverlayedText);

        ImageType imageType = ImageType.JPEG;
        if(documentName.toLowerCase().endsWith(".png"))
            imageType = ImageType.PNG;

        //Get image bytes
        ByteBuffer imageBytes = null;
        try(InputStream in = new FileInputStream(documentName)) {
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(in));
        }

        //Extract text
        List<TextLine> lines = extractText(imageBytes,wordBased);

        //Get Image
        BufferedImage image = getImage(documentName);

        //Create new pdf document
        PDFDocument pdfDocument = new PDFDocument(showOverlayedText);

        //Add page with text layer and image in the pdf document
        pdfDocument.addPage(image, imageType, lines);

        //Save PDF to local disk
        try(OutputStream outputStream = new FileOutputStream(outputDocumentName)) {
            pdfDocument.save(outputStream);
            pdfDocument.close();
        }

        System.out.println("Generated searchable pdf: " + outputDocumentName);
    }

    private BufferedImage getImage(String documentName) throws IOException {

        BufferedImage image = null;

        try(InputStream in = new FileInputStream(documentName)) {
            image = ImageIO.read(in);
        }

        return image;
    }

    private List<TextLine> extractText(ByteBuffer imageBytes, boolean wordBased) {

        AmazonTextract client = AmazonTextractClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("textract.us-west-2.amazonaws.com","us-west-2")).build();

        DetectDocumentTextRequest request = new DetectDocumentTextRequest()
                .withDocument(new Document()
                        .withBytes(imageBytes));

        DetectDocumentTextResult result = client.detectDocumentText(request);

        List<TextLine> lines = new ArrayList<TextLine>();
        List<Block> blocks = result.getBlocks();
        BoundingBox boundingBox = null;
        String blockTypeLevel = wordBased?"WORD":"LINE";
        System.out.printf("BlockTypeLevel: %s\n",blockTypeLevel);
        for (Block block : blocks) {

            if ((block.getBlockType()).equals(blockTypeLevel)) {
                boundingBox = block.getGeometry().getBoundingBox();
                lines.add(new TextLine(boundingBox.getLeft(),
                        boundingBox.getTop(),
                        boundingBox.getWidth(),
                        boundingBox.getHeight(),
                        block.getText()));
            }
        }

        return lines;
    }
}
