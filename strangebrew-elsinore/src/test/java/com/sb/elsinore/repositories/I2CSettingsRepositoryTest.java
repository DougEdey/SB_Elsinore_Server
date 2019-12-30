package com.sb.elsinore.repositories;

import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.I2CSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.validation.ConstraintViolationException;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@AutoConfigureTestDatabase
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
public class I2CSettingsRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private I2CSettingsRepository i2CSettingsRepository;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void canCreateI2CSettings() {
        I2CSettings i2cSettings = new I2CSettings();
        i2cSettings.setAddress(1);
        i2cSettings.setDeviceNumber(2);
        i2cSettings.setDeviceType("ADS1015");

        Long i2cSettingsId = this.entityManager.persistAndFlush(i2cSettings).getId();

        Optional<I2CSettings> optional = this.i2CSettingsRepository.findById(i2cSettingsId);

        assertTrue(optional.isPresent());
        assertEquals(i2cSettings.getAddress(), optional.get().getAddress());
        assertEquals(i2cSettings.getDeviceNumber(), optional.get().getDeviceNumber());
    }

    @Test
    public void canDeleteI2CSettings() {
        I2CSettings i2cSettings = new I2CSettings();
        i2cSettings.setAddress(1);
        i2cSettings.setDeviceNumber(2);
        i2cSettings.setDeviceType("ADS1015");

        Long i2cSettingsId = this.entityManager.persistAndFlush(i2cSettings).getId();

        this.i2CSettingsRepository.deleteById(i2cSettingsId);
        Optional<I2CSettings> optional = this.i2CSettingsRepository.findById(i2cSettingsId);

        assertFalse(optional.isPresent());
    }

    @Test
    public void i2cSettingsMustHaveAddress() {
        I2CSettings i2cSettings = new I2CSettings();
        i2cSettings.setDeviceNumber(2);
        i2cSettings.setDeviceType("ADS1015");

        this.thrown.expect(ConstraintViolationException.class);
        this.i2CSettingsRepository.saveAndFlush(i2cSettings);
    }

    @Test
    public void i2cSettingsMustHaveDeviceNumber() {
        I2CSettings i2cSettings = new I2CSettings();
        i2cSettings.setAddress(1);
        i2cSettings.setDeviceType("ADS1015");

        this.thrown.expect(ConstraintViolationException.class);
        this.i2CSettingsRepository.saveAndFlush(i2cSettings);
    }

    @Test
    public void i2cSettingsMustHaveDeviceType() {
        I2CSettings i2cSettings = new I2CSettings();
        i2cSettings.setAddress(1);
        i2cSettings.setDeviceNumber(2);

        this.thrown.expect(ConstraintViolationException.class);
        this.i2CSettingsRepository.saveAndFlush(i2cSettings);
    }
}
