export const Validators = {
  allowedKeys: ['Backspace', 'Tab', 'ArrowLeft', 'ArrowRight', 'Delete'],

  letterRegex: /^[A-Za-zÀ-ÖØ-öø-ÿ' -]$/,
  email_Regex: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  emailRegex: /^[a-zA-Z0-9@._+-]$/,
  fullNameRegex: /^(?!.*([ '-])\1)(?!.*(^|[^A-Za-zÀ-ÖØ-öø-ÿ])[ '-]|[ '-]([^A-Za-zÀ-ÖØ-öø-ÿ]|$))[A-Za-zÀ-ÖØ-öø-ÿ' -]{2,}$/,
  passwordRegex: /^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+=?.,:;{}\[\]<>\-]).{8,20}$/,
  hasLetter: /[A-Za-zÀ-ÖØ-öø-ÿ]/,
  numberRegex: /^(?!\.)\d+(?:\.\d{0,2})?$/
};