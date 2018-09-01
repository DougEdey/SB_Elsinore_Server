package com.sb.elsinore.repositories;


import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.OutputDeviceModel;
import com.sb.elsinore.models.PIDSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import javax.validation.ConstraintViolationException;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
@DataJpaTest
public class OutputDeviceRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private OutputDeviceRepository outputDeviceRepository;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void canCreateOutputDevice() {
        OutputDeviceModel outputDeviceModel = new OutputDeviceModel();
        outputDeviceModel.setName("Device");
        outputDeviceModel.setType("Type");
        outputDeviceModel.setPIDSettings(new PIDSettings());

        Long id = this.entityManager.persistAndFlush(outputDeviceModel).getId();

        Optional<OutputDeviceModel> outputDevice = this.outputDeviceRepository.findById(id);

        assertTrue(outputDevice.isPresent());
        assertEquals(outputDeviceModel.getName(), outputDevice.get().getName());
        assertEquals(outputDeviceModel.getType(), outputDevice.get().getType());
        assertEquals(outputDeviceModel.getPIDSettings(), outputDevice.get().getPIDSettings());
    }

    @Test
    public void outputDeviceRequiresName() {
        OutputDeviceModel outputDeviceModel = new OutputDeviceModel();

        outputDeviceModel.setType("Type");
        outputDeviceModel.setPIDSettings(new PIDSettings());

        this.thrown.expect(ConstraintViolationException.class);
        this.outputDeviceRepository.saveAndFlush(outputDeviceModel);
    }

    @Test
    public void outputDeviceRequiresType() {
        OutputDeviceModel outputDeviceModel = new OutputDeviceModel();

        outputDeviceModel.setName("Name");
        outputDeviceModel.setPIDSettings(new PIDSettings());

        this.thrown.expect(ConstraintViolationException.class);
        this.outputDeviceRepository.saveAndFlush(outputDeviceModel);
    }

    @Test
    public void outputDeviceRequiresPIDSettings() {
        OutputDeviceModel outputDeviceModel = new OutputDeviceModel();
        outputDeviceModel.setName("Device");
        outputDeviceModel.setType("Type");

        this.thrown.expect(ConstraintViolationException.class);
        this.outputDeviceRepository.saveAndFlush(outputDeviceModel);
    }
}
