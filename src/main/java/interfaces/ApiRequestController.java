package interfaces;

import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import utils.Metrics;
import imports.ResultStatus;

public interface ApiRequestController {

    ResultStatus<String> processApiRequest(final Map<String, Object> requestBody, final Metrics metrics)
        throws MissingApiRequestKeyException;
}
