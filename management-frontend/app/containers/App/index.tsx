import * as React from 'react';
import { Header } from '../../components';

const s = require('./style.css');

interface IProps {
  children?: any;
  location?: {
    pathname: string
  };
  params?: {
    node: string
  };
}

export class App extends React.Component<IProps, void> {
  render() {
    const { params, location, children } = this.props;
    return (
      <div className="pt-dark">
      <section className={s.App}>
        <Header />
          {children}
      </section>
      </div>
    );
  }
}

export default App;
