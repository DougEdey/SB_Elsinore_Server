package com.sb.elsinore.models;

import com.sb.elsinore.interfaces.OutputDeviceInterface;
import com.sb.elsinore.interfaces.PIDSettingsInterface;
import org.hibernate.annotations.Cascade;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name"}))
public class OutputDeviceModel implements OutputDeviceInterface {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Length(min = 1)
    private String name = "";

    @NotNull
    @Length(min = 1)
    private String type = "";

    @OneToOne(fetch = FetchType.EAGER, targetEntity = PIDSettings.class)
    @JoinColumn(name = "pid_settings_id")
    @NotNull
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    private PIDSettingsInterface pidSettings;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @NotNull
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Override
    public PIDSettingsInterface getPidSettings() {
        return this.pidSettings;
    }

    @Override
    public void setPidSettings(PIDSettingsInterface pidSettings) {
        this.pidSettings = pidSettings;
    }

    @NotNull
    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public void setType(@NotNull String type) {
        this.type = type;
    }
}
