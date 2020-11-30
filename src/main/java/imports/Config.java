package imports;

import com.amazonaws.regions.Regions;

public class Config {

    public static final String PUSH_SNS_PLATFORM_ARN = "arn:aws:sns:us-east-1:438338746171:app/GCM/LiteWeight";
    public static final String PUSH_EMAIL_PLATFORM_ARN = "arn:aws:sns:us-east-1:438338746171:LiteWeightDevEmail";
    public static final Regions REGION = Regions.US_EAST_1;
}
