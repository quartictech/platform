export const intentForDatasetStatus = (status) => {
  switch (status) {
    case "running": return "pt-intent-primary";
    case "success": return "pt-intent-success";
    case "failure": return "pt-intent-danger";
  }
};
