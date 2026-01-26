/*
 * Usage information:
 * This Script generates and cleans up Repositories, Custom-Properties for each generated Repository and Default-Properties.
 * The amount of each entity that gets generated can be changed, by changing the value of the const variables at the beginning of this script.
 * Per default this script tries to generate the entities for a local scm manager with its default credentials.
 * To change this, you have to adjust the value of the const variables at the beginning of this script.
 *
 * To generate the data, you have to call this script like this `node test_data.js generate`.
 * To clean up the data, you have to call this script like this `node test_data.js clean`.
 */

/* eslint-disable no-plusplus */
/* eslint-disable no-restricted-syntax */
/* eslint-disable no-await-in-loop */

const { Buffer } = require("node:buffer");

const BASE_URL = "http://localhost:8081/scm/api/v2";
const DEFAULT_CREDENTIALS = `Basic ${Buffer.from("scmadmin:scmadmin").toString("base64")}`;
const NAMESPACE_COUNT = 100;
const REPO_PER_NAMESPACE_COUNT = 10;
const CUSTOM_PROPS_PER_REPO_COUNT = 10;
const DEFAULT_PROPS_COUNT = 10;

async function scmFetch({ path, method, contentType, body }) {
  const headers = {
    Authorization: DEFAULT_CREDENTIALS,
  };

  if (contentType) {
    headers["Content-Type"] = contentType;
  }

  const response = await fetch(`${BASE_URL}/${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
}

async function createRepo({ name, namespace }) {
  try {
    await scmFetch({
      path: "repositories",
      method: "POST",
      contentType: "application/vnd.scmm-repository+json;v=2",
      body: {
        namespace,
        name,
        type: "git",
        contact: "",
        description: "",
      },
    });
    console.log(`Created repo ${namespace}/${name}`);
  } catch (e) {
    console.log(`Failed to create repo ${namespace}/${name}`);
    console.log(e.message);
  }
}

async function createCustomProp({ name, namespace, key, value }) {
  try {
    await scmFetch({
      path: `custom-properties/${namespace}/${name}`,
      method: "POST",
      contentType: "application/vnd.scmm-CustomProperty+json;v=2",
      body: { key, value },
    });
    console.log(`Created custom prop ${key}=${value} for repo ${namespace}/${name}`);
  } catch (e) {
    console.log(`Failed to create custom prop ${key}=${value} for repo ${namespace}/${name}`);
    console.log(e.message);
  }
}

// defaultProps: { key, defaultValue }[]
async function createDefaultProps(defaultProps) {
  const predefinedKeys = {};
  for (const defaultProp of defaultProps) {
    predefinedKeys[defaultProp.key] = { allowedValues: [], mode: "DEFAULT", defaultValue: defaultProp.defaultValue };
  }

  try {
    await scmFetch({
      path: "custom-properties/global-configuration",
      method: "PUT",
      contentType: "application/json",
      body: {
        enabled: true,
        enableNamespaceConfig: true,
        predefinedKeys,
      },
    });
    console.log("Created default props");
  } catch (e) {
    console.log("Failed to create default props");
    console.log(e.message);
  }
}

async function generate() {
  // Generate repos
  let repoPromises = [];
  for (let namespaceIdx = 0; namespaceIdx < NAMESPACE_COUNT; ++namespaceIdx) {
    for (let repoIdx = 0; repoIdx < REPO_PER_NAMESPACE_COUNT; ++repoIdx) {
      const namespace = `namespace-${namespaceIdx}`;
      const name = `repo-${repoIdx}`;
      repoPromises.push(createRepo({ namespace, name }));
    }
    await Promise.all(repoPromises);
    repoPromises = [];
  }

  // Generate custom properties
  let customPropsPromises = [];
  for (let namespaceIdx = 0; namespaceIdx < NAMESPACE_COUNT; ++namespaceIdx) {
    for (let repoIdx = 0; repoIdx < REPO_PER_NAMESPACE_COUNT; ++repoIdx) {
      const namespace = `namespace-${namespaceIdx}`;
      const name = `repo-${repoIdx}`;
      for (let customPropIdx = 0; customPropIdx < CUSTOM_PROPS_PER_REPO_COUNT; ++customPropIdx) {
        const key = `key-${customPropIdx}`;
        const value = `value-${customPropIdx}`;
        customPropsPromises.push(createCustomProp({ namespace, name, key, value }));
      }
      await Promise.all(customPropsPromises);
      customPropsPromises = [];
    }
  }

  // Generate Default Props
  const defaultProps = [];
  for (let defaultPropIdx = 0; defaultPropIdx < DEFAULT_PROPS_COUNT; ++defaultPropIdx) {
    const key = `defaultKey-${defaultPropIdx}`;
    const defaultValue = `defaulValue-${defaultPropIdx}`;
    defaultProps.push({ key, defaultValue });
  }
  await createDefaultProps(defaultProps);
}

async function deleteRepo({ name, namespace }) {
  try {
    await scmFetch({
      path: `repositories/${namespace}/${name}`,
      method: "DELETE",
    });
    console.log(`Deleted repo ${namespace}/${name}`);
  } catch (e) {
    console.log(`Failed to delete repo ${namespace}/${name}`);
    console.log(e.message);
  }
}

async function clean() {
  // Delete Repos
  let repoPromises = [];
  for (let namespaceIdx = 0; namespaceIdx < NAMESPACE_COUNT; ++namespaceIdx) {
    for (let repoIdx = 0; repoIdx < REPO_PER_NAMESPACE_COUNT; ++repoIdx) {
      const namespace = `namespace-${namespaceIdx}`;
      const name = `repo-${repoIdx}`;
      repoPromises.push(deleteRepo({ namespace, name }));
    }
    await Promise.all(repoPromises);
    repoPromises = [];
  }
}

if (process.argv.length !== 3) {
  console.log("Invalid argument count, expected 3. Third needs to be a command.");
  console.log("Available Commands: generate, clean");
  process.exit(1);
}

const command = process.argv[2];
if (command === "generate") {
  generate()
    .then(() => console.log("Finished generating"))
    .catch((error) => {
      console.log("Errors occurred during generation");
      console.log("Error", error);
    });
} else if (command === "clean") {
  clean()
    .then(() => console.log("Finished cleaning"))
    .catch((error) => {
      console.log("Errors occurred during cleaning");
      console.log("Error", error);
    });
} else {
  console.log("Unknown Command: ", command);
  console.log("Available Commands: generate, clean");
}
