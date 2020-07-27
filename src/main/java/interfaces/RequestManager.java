package interfaces;

import helpers.ResultStatus;

public interface RequestManager {

  public ResultStatus execute(String... args);
}
