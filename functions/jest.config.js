module.exports = {
  testEnvironment: 'node',
  // Cible uniquement notre fichier de tests pour éviter les suites héritées
  testMatch: ['**/test/credits.test.js'],
  verbose: true,
  clearMocks: true,
  // Désactive toute transformation Babel/Jest pour éviter la lecture de .babelrc
  transform: {},
  moduleFileExtensions: ['js', 'json'],
};


