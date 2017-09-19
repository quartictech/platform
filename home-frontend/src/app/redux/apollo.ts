import { ApolloClient, createNetworkInterface } from "react-apollo";
import { apiRootUrl, ApiError } from "./api";
import { QUARTIC_XSRF, QUARTIC_XSRF_HEADER } from "../helpers/Utils";

const networkInterface = createNetworkInterface({
  uri: `${apiRootUrl}/gql`,
  opts: {
    credentials: "same-origin",
  },
});

networkInterface.use([{
  applyMiddleware(req, next) {
    if (!req.options.headers) {
      req.options.headers = {};  // Create the header object if needed.
    }
    // get the authentication token from local storage if it exists
    const token = localStorage.getItem(QUARTIC_XSRF);
    req.options.headers[QUARTIC_XSRF_HEADER] = token;
    req.options.headers["Accept"] = "application/json";
    next();
  },
}]);

networkInterface.useAfter([{
  applyAfterware({ response }, next) {
    if (response.status === 401) {
      throw new ApiError("Unauthorized", 401);
    }
    next();
  },
}]);

export const client = new ApolloClient({ networkInterface, reduxRootSelector: state => state.get("apollo") });
