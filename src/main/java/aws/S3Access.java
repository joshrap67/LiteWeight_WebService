package aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import helpers.Metrics;
import helpers.ResultStatus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

public class S3Access {

    private final String JPG_TYPE = "jpg";
    private final String JPG_MIME = "image/jpeg";
    private final String PNG_TYPE = "png";
    private final String PNG_MIME = "image/png";

    private final AmazonS3 s3Client;
    private static final String S3_IMAGE_BUCKET = "liteweight-images";

    public S3Access() {
        this.s3Client = AmazonS3ClientBuilder
            .standard()
            .withRegion(Regions.US_EAST_2)
            .build();
    }

    public S3Access(final AmazonS3 amazonS3) {
        this.s3Client = amazonS3;
    }

    public String uploadImage(final byte[] fileData, final Metrics metrics) {
        final String classMethod = this.getClass().getSimpleName() + ".uploadImage";
        metrics.commonSetup(classMethod);

        String fileName;
        try {
            final UUID uuid = UUID.randomUUID();
            fileName = uuid.toString() + "." + JPG_TYPE;

            InputStream is = new ByteArrayInputStream(fileData);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(fileData.length);
            objectMetadata.setContentType(JPG_MIME);

            PutObjectRequest putObjectRequest = new PutObjectRequest(S3_IMAGE_BUCKET, fileName, is,
                objectMetadata).withCannedAcl(CannedAccessControlList.PublicRead);

            this.s3Client.putObject(putObjectRequest);
            is.close();
        } catch (Exception e) {
            fileName = null;
            metrics.log("Error" + e);
        }

        metrics
            .commonClose(fileName != null ? ResultStatus.SUCCESS_CODE : ResultStatus.BAD_REQUEST);
        return fileName;
    }

    public boolean updateImage(final byte[] fileData, final String fileName, final Metrics metrics) {
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

        metrics
            .commonClose(fileName != null ? ResultStatus.SUCCESS_CODE : ResultStatus.BAD_REQUEST);
        return success;
    }

    public Boolean imageBucketExists() {
        return this.s3Client.doesBucketExistV2(S3_IMAGE_BUCKET);
    }
}
