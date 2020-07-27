package modules;

import helpers.Metrics;

public class Injector {
  public static LiteWeightComponent getInjector(final Metrics metrics) {
    return DaggerLiteWeightComponent
        .builder()
        .liteWeightModule(new LiteWeightModule(metrics))
        .build();
  }
}
