package interfaces;

import exceptions.MissingApiRequestKeyException;
import java.util.Map;
import helpers.Metrics;
import helpers.ResultStatus;

public interface ApiRequestController {

  ResultStatus processApiRequest(final Map<String, Object> requestBody, final Metrics metrics)
      throws MissingApiRequestKeyException;
}
