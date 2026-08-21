package ru.holyworld.invisindicator;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvisIndicatorClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("invisindicator");

    @Override
    public void onInitializeClient() {
        RingRenderer.register();
        LOGGER.info("Invis Indicator initialized. Edit config/invisindicator.json to change color/radius/alpha.");
    }
}
