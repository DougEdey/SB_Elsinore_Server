import {Component} from "react";
import * as React from "react";
import {Form} from "react-bootstrap";

type Props = {
    value: string | null | undefined;
    updateName: Function;
}

export class ProbeNameField extends Component<Props> {

    render() {
        let {updateName, value} = this.props;
        if (value == null) {
            value = "" 
        }
        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.name">
                <Form.Label>Name</Form.Label>
                <Form.Control type="text"
                    placeholder="Probe Name"
                    defaultValue={value.toString()}
                    onBlur={(event: React.FocusEvent<HTMLInputElement>) => updateName(event.target.value)} />
            </Form.Group>
        );
    }


    getErrorMessage = (value: String) => {
        if (value == null || value.trim() === "") {
            return "Name cannot be empty";
        }
        return "";
    }
}
