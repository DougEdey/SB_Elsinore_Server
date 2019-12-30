package com.sb.elsinore.graphql;

import com.sb.elsinore.models.SystemSettings;
import com.sb.elsinore.repositories.SystemSettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class SystemSettingsQueries {
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    public SystemSettingsQueries(SystemSettingsRepository systemSettingsRepository) {
        this.systemSettingsRepository = systemSettingsRepository;
    }

    public SystemSettings getSystemSettings() {
        return this.systemSettingsRepository.findAll().stream().findFirst().orElse(new SystemSettings());
    }

    public SystemSettings updateSystemSettings(String breweryName, Boolean recorderEnabled, String scale,
                                               Boolean restoreState, Boolean owfsEnabled, String owfsServer,
                                               Integer owfsPort, Integer serverPort, String theme) {
        SystemSettings currentSystemSettings = getSystemSettings();

        if (breweryName != null) {
            currentSystemSettings.setBreweryName(breweryName);
        }
        if (recorderEnabled != null) {
            currentSystemSettings.setRecorderEnabled(recorderEnabled);
        }
        if (scale != null) {
            currentSystemSettings.setScale(scale);
        }
        if (restoreState != null) {
            currentSystemSettings.setRestoreState(restoreState);
        }
        if (owfsEnabled != null) {
            currentSystemSettings.setOwfsEnabled(owfsEnabled);
        }
        if (owfsServer != null) {
            currentSystemSettings.setOwfsServer(owfsServer);
        }
        if (owfsPort != null) {
            currentSystemSettings.setOwfsPort(owfsPort);
        }
        if (serverPort != null) {
            currentSystemSettings.setServerPort(serverPort);
        }
        if (theme != null) {
            currentSystemSettings.setTheme(theme);
        }

        this.systemSettingsRepository.save(currentSystemSettings);
        return currentSystemSettings;
    }

}
