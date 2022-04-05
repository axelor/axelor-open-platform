package com.axelor;

import com.axelor.app.AppSettings;
import com.axelor.auth.AuthUtils;
import java.lang.reflect.Field;

public class TestingHelpers {

  private TestingHelpers() {}

  /** Reset AppSettings, else we can get properties set from other tests */
  public static void resetSettings() {
    try {
      Field instance = AppSettings.class.getDeclaredField("instance");
      instance.setAccessible(true);
      instance.set(null, null);
    } catch (Exception e) {
      throw new RuntimeException();
    }
  }

  @SuppressWarnings("ConstantConditions")
  public static void logout() {
    try {
      AuthUtils.getSubject().logout();
    } catch (Exception e) {
      // ignore
    }
  }
}
