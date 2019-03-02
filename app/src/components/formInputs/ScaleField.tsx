import {Component} from "react";
import * as React from "react";
import {Form} from 'react-bootstrap';
import Switch from 'react-bootstrap-switch';

type Props = {
    value: String;
    updateScale: Function;
}

export class ScaleField extends Component<Props> {


    _onChange(e, value){
        const {updateScale} = this.props;
        let newScale = value ? "F" : "C";
        
        updateScale(newScale)
    }
    
    render() {
        const {value} = this.props;
        let booleanValue = (value == "F" ? true : false);
        //const errorMessage = this.getErrorMessage(value);
        return (
            <Form.Group controlId="probe.device">
                <Switch labelText="Scale" 
                    onText="F"
                    offText="C"
                    onChange={this._onChange.bind(this)}
                    defaultValue={booleanValue}/>
            </Form.Group>
        );
    }
}