---
title: Overview 
---

With this plugin it is possible to create a collection of user-defined properties for each repository.
These properties are defined as key-value pairs.
To see an overview of the existing properties in tabular form,
you can click on the 'Custom Properties' tab in the repository navigation.

![Table of already created custom properties of a repository](./assets/custom-properties-overview.png)

In this overview it is possible to view, edit and delete already created properties or create a new property.
With the 'Add new custom property' button, which is located below the table,
the user can navigate to another view to create a new property.

![Unfilled view for creating a new custom property](./assets/custom-properties-create.png)

In this view, the key and the corresponding value can be defined using two text inputs.
The property is created by clicking the 'Submit' button.
When creating the property, make sure that the key 
1. is not longer than 255 characters,
2. consists only of letters, numbers, periods, spaces and underscores,
3. is unique within a repository(each key can be assigned only once).

Multiple values for the same key must be entered as a single property separated by commas. 
There are no restrictions on the value.

For each custom property in the overview, there is a separate button for editing and deleting properties.
A property is edited in a similar way as it is created.

To edit, delete and create properties, it is necessary to have the permission to modify the repository metadata.
If the user does not have this authorisation, the corresponding buttons will not be displayed.
