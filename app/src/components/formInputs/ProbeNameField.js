import {Component} from "react";
import {TextField} from "@shopify/polaris";
import React from "react";

class ProbeNameField extends Component {
    state = {
        value: this.props.probe.name,
    };

    render() {
        const {value} = this.state;
        const errorMessage = this.getErrorMessage(value);
        return (
            <TextField label="Name"
                       id="ProbeName"
                       value={value}
                       onChange={this.handleChange}
                       error={errorMessage} />
        );
    }

    handleChange = (value) => {
        this.setState({value});
    };

    getErrorMessage = (value) => {
      if (value == null || value.trim() === "") {
          return "Name cannot be empty";
      }
      return "";
    }
}

export default ProbeNameField;