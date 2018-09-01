package com.sb.elsinore.models;

import com.sb.elsinore.interfaces.SwitchInterface;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"}),
        @UniqueConstraint(columnNames = {"gpioName"})
}
)
public class SwitchModel implements SwitchInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @javax.validation.constraints.Size(min = 1)
    private String name = "";

    @NotNull
    @javax.validation.constraints.Size(min = 1)
    private String gpioName = "";


    private boolean invertOutput = false;
    private int position = -1;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getGPIOName() {
        return this.gpioName;
    }

    @Override
    public void setGPIOName(String gpioName) {
        this.gpioName = gpioName;
    }

    @Override
    public boolean isOutputInverted() {
        return this.invertOutput;
    }

    @Override
    public void setOutputInverted(boolean inverted) {
        this.invertOutput = inverted;
    }

    @Override
    public int getPosition() {
        return this.position;
    }

    @Override
    public void setPosition(int position) {
        this.position = position;
    }
}
