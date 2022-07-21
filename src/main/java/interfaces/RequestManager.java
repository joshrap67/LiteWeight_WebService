package interfaces;

import imports.ResultStatus;

public interface RequestManager {

    ResultStatus execute(String... args);
}
