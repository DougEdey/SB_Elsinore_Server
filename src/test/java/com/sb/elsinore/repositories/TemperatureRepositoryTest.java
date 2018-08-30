package com.sb.elsinore.repositories;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.TemperatureListeners;
import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.Temperature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
@DataJpaTest
public class TemperatureRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Mock
    LaunchControl launchControl;
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TemperatureRepository temperatureRepository;

    @InjectMocks
    private TemperatureListeners temperatureListeners;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void canCreateTemperature() {
        // given
        Temperature temperature = new Temperature();
        temperature.setName("Test");
        temperature.setDevice("Device");

        this.entityManager.persist(temperature);
        this.entityManager.flush();

        // when
        Temperature found = this.temperatureRepository.findByName("Test");

        //then
        assertEquals(temperature.getName(), found.getName());
    }

}
