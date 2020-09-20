package aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import helpers.Config;
import helpers.Metrics;
import helpers.ResultStatus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class S3Access {

    private final String JPG_MIME = "image/jpeg";

    private final AmazonS3 s3Client;
    private static final String S3_IMAGE_BUCKET = "liteweight-images";
    public static final String JPG_TYPE = "jpg";

    public S3Access() {
        this.s3Client = AmazonS3ClientBuilder
            .standard()
            .withRegion(Config.REGION)
            .build();
    }

    public S3Access(final AmazonS3 amazonS3) {
        this.s3Client = amazonS3;
    }

    public boolean uploadImage(final byte[] fileData, final String fileName,
        final Metrics metrics) {
        final String classMethod = this.getClass().getSimpleName() + ".uploadImage";
        metrics.commonSetup(classMethod);
        boolean success = false;

        try {
            InputStream is = new ByteArrayInputStream(fileData);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(fileData.length);
            objectMetadata.setContentType(JPG_MIME);

            PutObjectRequest putObjectRequest = new PutObjectRequest(S3_IMAGE_BUCKET, fileName, is,
                objectMetadata).withCannedAcl(CannedAccessControlList.PublicRead);

            this.s3Client.putObject(putObjectRequest);
            success = true;
            is.close();
        } catch (Exception e) {
            metrics.log("Error" + e);
        }

        metrics.commonClose(fileName != null);
        return success;
    }
}
