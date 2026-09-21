package dev.lcdsmao.immersia;

interface IImmersiaShizukuService {
    boolean isSplitImmersiveModeEnabled();

    void setSplitImmersiveMode(boolean enabled);

    String getSecureSetting(String key);

    void putSecureSetting(String key, String value);

    void deleteSecureSetting(String key);
}
