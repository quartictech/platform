import * as React from 'react';
import { IndexRoute, Route } from 'react-router';
import { App, Home, About } from '../containers';

function getRoutes(store) {
  return (
    <Route path="/" component={App}>
      <IndexRoute component={Home} />
      <Route path="about" component={About} />
      <Route path="*" component={Home} />
    </Route>
  );
};

export { getRoutes }
