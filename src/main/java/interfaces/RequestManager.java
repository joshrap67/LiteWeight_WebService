package interfaces;

import imports.ResultStatus;

public interface RequestManager {

    public ResultStatus execute(String... args);
}
