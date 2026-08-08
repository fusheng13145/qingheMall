module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-recommended',
    'prettier'
  ],
  parserOptions: {
    ecmaVersion: 2022,
    sourceType: 'module'
  },
  globals: {
    // Vitest globals（若启用 globals: true）
    describe: 'readonly',
    it: 'readonly',
    expect: 'readonly',
    vi: 'readonly',
    beforeEach: 'readonly'
  },
  rules: {
    'vue/multi-word-component-names': 'off',
    'vue/no-v-html': 'off',
    'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    'no-console': process.env.NODE_ENV === 'production' ? 'warn' : 'off'
  },
  ignorePatterns: ['dist/', 'dist-*/', 'node_modules/']
}
