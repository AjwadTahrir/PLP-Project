// Empty pages reserved in the shared interface for other groups' features.
// Each owning group fills its page by calling registerView() with the same id (see frontend/README.md):
//   exercises -> Groups 4 & 6, lecturer -> Groups 4, 5 & 6, compare -> Group 9.

const empty = () => {};

export const placeholderViews = [
  { id: "exercises", title: "Exercises", render: empty },
  { id: "lecturer", title: "Lecturer", render: empty },
  { id: "compare", title: "Compare", render: empty },
];
