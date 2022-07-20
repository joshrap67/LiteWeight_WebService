package services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.IOUtils;
import imports.Config;
import java.io.IOException;
import utils.Metrics;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class StorageService {

    private final AmazonS3 s3Client;
    private static final String S3_IMAGE_BUCKET = "liteweight-images";
    private static final String S3_DEFAULT_IMAGE_BUCKET = "liteweight-images-private";
    private static final String S3_DEFAULT_IMAGE_FILE = "DefaultProfilePicture.jpg";
    public static final String JPG_TYPE = "jpg";

    public StorageService() {
        this.s3Client = AmazonS3ClientBuilder
            .standard()
            .withRegion(Config.REGION)
            .build();
    }

    public boolean uploadImage(final byte[] fileData, final String fileName, final Metrics metrics) throws IOException {
        final String classMethod = this.getClass().getSimpleName() + ".uploadImage";
        metrics.commonSetup(classMethod);

        InputStream is = new ByteArrayInputStream(fileData);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(fileData.length);
        objectMetadata.setContentType("image/jpeg");

        PutObjectRequest putObjectRequest = new PutObjectRequest(S3_IMAGE_BUCKET, fileName, is,
            objectMetadata).withCannedAcl(CannedAccessControlList.PublicRead);

        this.s3Client.putObject(putObjectRequest);
        is.close();

        metrics.commonClose(true);
        return true;
    }

    public byte[] downloadDefaultImage(final Metrics metrics) throws IOException {
        final String classMethod = this.getClass().getSimpleName() + ".downloadDefaultImage";
        metrics.commonSetup(classMethod);

        GetObjectRequest request = new GetObjectRequest(S3_DEFAULT_IMAGE_BUCKET, S3_DEFAULT_IMAGE_FILE);
        S3Object s3Object = this.s3Client.getObject(request);
        s3Object.getObjectContent();
        byte[] defaultImageBytes = IOUtils.toByteArray(s3Object.getObjectContent());

        metrics.commonClose(defaultImageBytes.length > 0);
        return defaultImageBytes;
    }
}
