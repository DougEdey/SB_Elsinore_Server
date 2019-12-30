package com.sb.elsinore.models;

import com.sb.elsinore.interfaces.SwitchInterface;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name"}),
        @UniqueConstraint(columnNames = {"gpio"})
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
    private String gpio = "";


    private boolean outputInverted = false;
    private int position = -1;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
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
    public String getGpio() {
        return this.gpio;
    }

    @Override
    public void setGpio(String gpio) {
        this.gpio = gpio;
    }

    @Override
    public boolean isOutputInverted() {
        return this.outputInverted;
    }

    @Override
    public void setOutputInverted(boolean inverted) {
        this.outputInverted = inverted;
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
