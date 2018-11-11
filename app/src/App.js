import React, {Component} from 'react';
import './App.css';
import Sidebar from "./Sidebar";
import TempProbeList from "./components/TempProbeList";

class App extends Component {
    render() {
        return (
            <div className="App">
                <Sidebar/>
                <TempProbeList/>
            </div>
        )
    }
}

export default App;
