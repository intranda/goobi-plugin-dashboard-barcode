package de.intranda.digiverso.model.helper;

import org.apache.commons.configuration.XMLConfiguration;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DashboardHelperBarcode {
    private String message = "hello world";

    public DashboardHelperBarcode() {
        log.info("creating DashboardHelperBarcode");
    }

    public DashboardHelperBarcode(XMLConfiguration config) {
        log.info("creating DashboardHelperBarcode using config");
    }

    public void execute() {
        log.info(message);
    }
}
