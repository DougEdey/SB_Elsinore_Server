package com.sb.elsinore.repositories;

import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.SwitchModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
@AutoConfigureTestDatabase
@RunWith(SpringJUnit4ClassRunner.class)
public class SwitchRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private SwitchRepository switchRepository;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void canCreateSwitchAndGetById() {
        // given
        SwitchModel switchModel = new SwitchModel();
        switchModel.setName("TestSwitch");
        switchModel.setGpio("GPIOName");

        Long switchId = this.entityManager.persistAndFlush(switchModel).getId();

        // when
        Optional<SwitchModel> found = this.switchRepository.findById(switchId);

        //then
        assertTrue(found.isPresent());
        assertEquals(switchModel, found.get());
    }


    @Test
    public void canDeleteSwitch() {
        // given
        SwitchModel switchModel = new SwitchModel();
        switchModel.setName("TestSwitch");
        switchModel.setGpio("GPIOName");

        Long switchId = this.entityManager.persistAndFlush(switchModel).getId();

        // when
        this.switchRepository.delete(switchModel);

        //then
        this.thrown.expect(JpaObjectRetrievalFailureException.class);
        this.switchRepository.getOne(switchId);
    }

    @Test
    public void switchNameMustBeUnique() {
        // given
        SwitchModel switchModel = new SwitchModel();
        switchModel.setName("TestSwitch");
        switchModel.setGpio("GPIOName");

        this.entityManager.persistAndFlush(switchModel);

        switchModel = new SwitchModel();
        switchModel.setName("TestSwitch");
        switchModel.setGpio("GPIOName2");

        this.thrown.expect(DataIntegrityViolationException.class);
        this.switchRepository.saveAndFlush(switchModel);

        assertEquals(1, this.switchRepository.findAll().size());
    }

    @Test
    public void switchRequiresAName() {
        // given
        SwitchModel switchModel = new SwitchModel();
        switchModel.setName("");
        switchModel.setGpio("GPIOName");

        this.thrown.expect(ConstraintViolationException.class);
        this.switchRepository.saveAndFlush(switchModel);
    }


    @Test
    public void switchRequiresAGPIOName() {
        // given
        SwitchModel switchModel = new SwitchModel();
        switchModel.setName("Test");
        switchModel.setGpio("");

        this.thrown.expect(ConstraintViolationException.class);
        this.switchRepository.saveAndFlush(switchModel);
    }
}
