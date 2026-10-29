/*
 * Copyright (c) 2020 - present Cloudogu GmbH
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

// Lightweight test doubles for the @scm-manager/ui-core exports used by the plugin. They render just
// enough real DOM (with correct roles/labels and working handlers) to exercise the plugin's own logic.
// The SubpageForm family is backed by a real react-hook-form instance so useFieldArray / Controller /
// useScmFormContext behave like production. LinkButton uses the real react-router Link so navigation works.
import React, { createContext, useCallback, useContext, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useController, useForm } from "react-hook-form";
import { vi } from "vitest";

type Any = Record<string, any>;

export const Button: React.FC<Any> = ({ children, onClick, disabled, type, variant, isLoading, testId, ...rest }) => (
  <button type={type ?? "button"} onClick={onClick} disabled={disabled} {...rest}>
    {children}
  </button>
);

export const LinkButton: React.FC<Any> = ({ to, children, variant, isLoading, testId, className, ...rest }) => (
  <Link to={to} className={className} {...rest}>
    {children}
  </Link>
);

export const IconButton: React.FC<Any> = ({ children, onClick, variant, size, testId, ...rest }) => (
  <button type="button" onClick={onClick} {...rest}>
    {children}
  </button>
);

export const Icon: React.FC<Any> = ({ children, className, ...rest }) => (
  <span data-icon className={className} {...rest}>
    {children}
  </span>
);

export const Level: React.FC<Any> = ({ left, right, children, className }) => (
  <div className={className}>
    {left}
    {children}
    {right}
  </div>
);

export const Subtitle: React.FC<Any> = ({ children }) => <h2>{children}</h2>;
export const Title: React.FC<Any> = ({ children }) => <h1>{children}</h1>;
export const Loading: React.FC<Any> = () => <div role="status">Loading…</div>;

export const ErrorNotification: React.FC<Any> = ({ error }) =>
  error ? <div role="alert">{error.message ?? String(error)}</div> : null;

export const Dialog: React.FC<Any> = ({ trigger, title, footer, children, open, onOpenChange }) => (
  <>
    {React.cloneElement(trigger, { onClick: () => onOpenChange?.(true) })}
    {open ? (
      <div role="dialog" aria-label={title}>
        <div>{children}</div>
        <div>{footer}</div>
      </div>
    ) : null}
  </>
);

export const useDocumentTitleForRepository = (..._args: unknown[]) => {
  /* no-op in tests */
};

export const Input: React.FC<Any> = ({ id, value, onChange, disabled, form, ...rest }) => (
  <input id={id} value={value ?? ""} onChange={onChange} disabled={disabled} {...rest} />
);

export const Label: React.FC<Any> = ({ htmlFor, children, as, ...rest }) => (
  <label htmlFor={htmlFor} {...rest}>
    {children}
  </label>
);

export const Checkbox: React.FC<Any> = ({ label, checked, onChange, disabled }) => (
  <label>
    <input type="checkbox" checked={!!checked} onChange={onChange} disabled={disabled} />
    {label}
  </label>
);

export const SelectField: React.FC<Any> = ({ label, id, value, onChange, disabled, children }) => (
  <label>
    {label}
    <select id={id} value={value} onChange={onChange} disabled={disabled}>
      {children}
    </select>
  </label>
);

export const Combobox: React.FC<Any> = ({ id, value, onChange, onQueryChange, options, disabled, readOnly, className }) => {
  const suggestions: Any[] = typeof options === "function" ? options() : Array.isArray(options) ? options : [];
  return (
    <>
      <input
        id={id}
        role="combobox"
        className={className}
        value={value?.value ?? ""}
        disabled={disabled}
        readOnly={readOnly}
        onChange={(e) => {
          onQueryChange?.(e.target.value);
          onChange?.({ label: e.target.value, value: e.target.value });
        }}
      />
      <ul aria-label="key-suggestions">
        {suggestions.map((option) => (
          <li key={option.value}>{option.label}</li>
        ))}
      </ul>
    </>
  );
};

// A minimal chip/tag input: shows current values and lets a test add one by typing + Enter.
export const ChipInputField: React.FC<Any> = ({ id, label, value = [], onChange, readOnly, error }) => {
  const [draft, setDraft] = useState("");
  return (
    <div>
      <span>{label}</span>
      <ul>
        {value.map((option: Any) => (
          <li key={option.value}>{option.label}</li>
        ))}
      </ul>
      <input
        id={id}
        aria-label={label}
        value={draft}
        readOnly={readOnly}
        onChange={(e) => setDraft(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter" && draft) {
            e.preventDefault();
            onChange?.([...value, { label: draft, value: draft }]);
            setDraft("");
          }
        }}
      />
      {error ? <p role="alert">{error}</p> : null}
    </div>
  );
};

const RadioGroupComponent: React.FC<Any> = ({ value, onValueChange, children, id, className }) => (
  <div role="radiogroup" id={id} className={className}>
    {React.Children.map(children, (child) =>
      React.isValidElement(child)
        ? React.cloneElement(child as any, { _groupValue: value, _onValueChange: onValueChange })
        : child,
    )}
  </div>
);
const RadioGroupOption: React.FC<Any> = ({ value, label, disabled, id, _groupValue, _onValueChange }) => (
  <label>
    <input
      type="radio"
      id={id}
      checked={_groupValue === value}
      disabled={disabled}
      onChange={() => _onValueChange?.(value)}
    />
    {label}
  </label>
);
export const RadioGroup = Object.assign(RadioGroupComponent, { Option: RadioGroupOption });

export const getRepositoryBaseUrl = (repository: Any) => `/repo/${repository.namespace}/${repository.name}`;

export const useRepositoryFromParams = vi.fn(() => ({
  namespace: "hitchhiker",
  name: "heart-of-gold",
  _embedded: { customProperties: { properties: [] }, missingMandatoryProperties: { missing: [] } },
}));

const CardComponent: React.FC<Any> = ({ children }) => <div>{children}</div>;
export const CardList = Object.assign(({ children }: Any) => <div>{children}</div>, {
  Card: Object.assign(CardComponent, {
    Row: ({ children }: Any) => <div>{children}</div>,
    Title: ({ children }: Any) => <div>{children}</div>,
  }),
});

export const Notification: React.FC<Any> = ({ type, children }) => (
  <div role="note" data-type={type}>
    {children}
  </div>
);

export const useNamespaceFromParams = vi.fn(() => ({
  namespace: "hitchhiker",
  _links: {
    customPropertiesConfig: { href: "/api/v2/namespace-config" },
    missingMandatoryProperties: { href: "/api/v2/namespace-missing" },
  },
}));

// ---------------------------------------------------------------------------
// SubpageForm family, backed by a real react-hook-form instance.
// ---------------------------------------------------------------------------

const ScmFormContext = createContext<Any | null>(null);
const SubpageContext = createContext<Any | null>(null);

export const useScmFormContext = () => useContext(ScmFormContext) as Any;
export const useSubpageContext = () => useContext(SubpageContext) as Any;

const SubpageFormRoot: React.FC<Any> = ({ children, title, translationPath, onSubmit, defaultValues, readOnly }) => {
  const form = useForm({ mode: "onChange", defaultValues });
  const [activeSubpage, setActiveSubpage] = useState<string | null>(null);
  const [subpageTitles, setSubpageTitles] = useState<Record<string, string>>({});

  const registerSubpage = useCallback(
    (name: string, subTitle: string) =>
      setSubpageTitles((prev) => (prev[name] === subTitle ? prev : { ...prev, [name]: subTitle })),
    [],
  );
  const openSubpage = useCallback((name: string) => setActiveSubpage(name), []);
  const closeSubpage = useCallback(() => {
    form.reset();
    setActiveSubpage(null);
  }, [form]);
  const saveActiveSubpage = useCallback(async () => {
    let ok = false;
    await form.handleSubmit(async (data) => {
      await onSubmit(data);
      form.reset(data);
      ok = true;
    })();
    if (ok) {
      setActiveSubpage(null);
    }
  }, [form, onSubmit]);

  // Read these so react-hook-form's formState proxy subscribes this root component and re-renders
  // (and therefore the context consumers, e.g. a Subpage's Submit button) when they change.
  const { isSubmitting, isDirty: _isDirty, isValid: _isValid } = form.formState;
  void _isDirty;
  void _isValid;
  // Subscribe to all value changes so context consumers that call watch() (e.g. PredefinedKeyFields
  // reacting to the selected mode) re-render when a field value changes.
  void form.watch();
  const effectiveReadOnly = isSubmitting || !!readOnly;
  const scm = { ...form, readOnly: effectiveReadOnly, t: (key: string) => key, formId: translationPath?.[1] ?? "form" };
  const sub = {
    title,
    activeSubpage,
    subpageTitles,
    registerSubpage,
    openSubpage,
    closeSubpage,
    saveActiveSubpage,
    error: null,
  };

  return (
    <ScmFormContext.Provider value={scm}>
      <SubpageContext.Provider value={sub}>
        <nav aria-label="breadcrumb">{title}</nav>
        {children}
      </SubpageContext.Provider>
    </ScmFormContext.Provider>
  );
};

const SubpageOverview: React.FC<Any> = ({ children }) => {
  const { activeSubpage } = useSubpageContext();
  return activeSubpage !== null ? null : <>{children}</>;
};

const Subpage: React.FC<Any> = ({ name, title, children }) => {
  const { activeSubpage, registerSubpage, closeSubpage, saveActiveSubpage } = useSubpageContext();
  const { readOnly, formState } = useScmFormContext();
  useEffect(() => registerSubpage(name, title), [name, title, registerSubpage]);
  if (activeSubpage !== name) {
    return null;
  }
  return (
    <>
      {children}
      {!readOnly ? (
        <div>
          <button type="button" onClick={closeSubpage}>
            Cancel
          </button>
          <button
            type="button"
            onClick={saveActiveSubpage}
            disabled={!formState.isDirty || !formState.isValid}
          >
            Submit
          </button>
        </div>
      ) : null}
    </>
  );
};

const SubpageOpenButton: React.FC<Any> = ({ subpage, children, ...rest }) => {
  const { openSubpage } = useSubpageContext();
  const { readOnly } = useScmFormContext();
  if (readOnly) {
    return null;
  }
  return (
    <button type="button" onClick={() => openSubpage(subpage)} {...rest}>
      {children}
    </button>
  );
};

const SubpageOpenIconButton: React.FC<Any> = ({ subpage, children, ...rest }) => {
  const { openSubpage } = useSubpageContext();
  const { readOnly } = useScmFormContext();
  if (readOnly) {
    return null;
  }
  return (
    <button type="button" onClick={() => openSubpage(subpage)} {...rest}>
      {children}
    </button>
  );
};

const SubpageHeader: React.FC<Any> = ({ subpage, label, children, icon, className, ...rest }) => (
  <div>
    <span>{label}</span>
    {subpage && children ? (
      <SubpageOpenButton subpage={subpage} className={className} {...rest}>
        {children}
      </SubpageOpenButton>
    ) : null}
  </div>
);

const SubpageTableComponent: React.FC<Any> = ({ children, className, ...rest }) => (
  <table className={className} {...rest}>
    {children}
  </table>
);
const SubpageTableEmptyRow: React.FC<Any> = ({ colSpan = 1000, className, children }) => (
  <tr>
    <td colSpan={colSpan} className={className}>
      {children}
    </td>
  </tr>
);
const SubpageTable = Object.assign(SubpageTableComponent, { EmptyRow: SubpageTableEmptyRow });
const SubpageDivider: React.FC<Any> = () => <hr />;

export const SubpageForm = Object.assign(SubpageFormRoot, {
  Overview: SubpageOverview,
  SubpageTable,
  Divider: SubpageDivider,
  Subpage: Object.assign(Subpage, {
    OpenButton: SubpageOpenButton,
    OpenIconButton: SubpageOpenIconButton,
    Header: SubpageHeader,
  }),
});

const BooleanRadio: React.FC<Any> = ({ name, label, trueLabel, falseLabel }) => {
  const { control } = useScmFormContext();
  const { field } = useController({ name, control });
  return (
    <fieldset>
      <legend>{label}</legend>
      <label>
        <input type="radio" name={name} checked={field.value === true} onChange={() => field.onChange(true)} />
        {trueLabel}
      </label>
      <label>
        <input type="radio" name={name} checked={field.value === false} onChange={() => field.onChange(false)} />
        {falseLabel}
      </label>
    </fieldset>
  );
};

export const Form = { BooleanRadio };
