// Mocks pour modules Firebase Functions
jest.mock('firebase-functions/params', () => ({
  defineSecret: () => ({ value: () => 'DUMMY' }),
}));

jest.mock('firebase-functions/v2/https', () => ({
  onRequest: (_opts, handler) => handler,
}));

class HttpsError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}

jest.mock('firebase-functions/v1', () => ({
  https: {
    onCall: (fn) => fn,
    HttpsError: class HttpsError extends Error {
      constructor(code, message) {
        super(message);
        this.code = code;
      }
    },
  },
  auth: {
    user: () => ({ onCreate: (fn) => fn }),
  },
}));

// Mocks pour firebase-admin
jest.mock('firebase-admin/app', () => ({ initializeApp: () => {} }));

const incrementSymbol = Symbol('increment');

const createMockDb = (stateByUid = {}) => {
  return {
    runTransaction: async (fn) => {
      const tx = {
        get: async (ref) => {
          const uid = ref.__id;
          const data = stateByUid[uid];
          return {
            exists: !!data,
            get: (field) => (data ? data[field] : undefined),
          };
        },
        update: (ref, updateObj) => {
          const uid = ref.__id;
          const current = stateByUid[uid] || {};
          const next = { ...current };
          if ('credits' in updateObj) {
            const inc = updateObj.credits?.[incrementSymbol] || 0;
            next.credits = (current.credits || 0) + inc;
          }
          stateByUid[uid] = next;
        },
      };
      return fn(tx);
    },
    collection: (name) => ({
      doc: (id) => ({
        __id: id,
        get: async () => {
          const data = stateByUid[id];
          return { exists: !!data, data: () => data };
        },
        set: async (val) => {
          stateByUid[id] = { ...(stateByUid[id] || {}), ...val };
        },
        update: async (val) => {
          stateByUid[id] = { ...(stateByUid[id] || {}), ...val };
        },
      }),
    }),
  };
};

// Mock firebase-admin/firestore avec FieldValue
jest.mock('firebase-admin/firestore', () => {
  return {
    getFirestore: () => global.__MOCK_DB__,
    FieldValue: {
      serverTimestamp: () => new Date(),
      increment: (n) => ({ [incrementSymbol]: n }),
    },
  };
});

// Mock @google/generative-ai pour éviter tout appel sortant
jest.mock('@google/generative-ai', () => ({
  GoogleGenerativeAI: class {
    getGenerativeModel() {
      return {
        generateContent: async () => ({ response: { text: () => '{}' } }),
      };
    }
  },
}));

describe('Credits Cloud Functions', () => {
  let decrementCredits;
  let getCredits;

  beforeEach(() => {
    global.__MOCK_DB__ = createMockDb({
      'user-positive': { credits: 2 },
      'user-zero': { credits: 0 },
    });
    // Re-require le module pour réinitialiser avec le nouveau DB mock
    jest.isolateModules(() => {
      const fns = require('..');
      decrementCredits = fns.decrementCredits;
      getCredits = fns.getCredits;
    });
  });

  test('decrementCredits réussit quand credits > 0', async () => {
    const context = { auth: { uid: 'user-positive' } };
    const res = await decrementCredits({}, context);
    expect(res).toEqual({ success: true });
    const snap = await global.__MOCK_DB__.collection('users').doc('user-positive').get();
    expect(snap.data().credits).toBe(1);
  });

  test('decrementCredits échoue si crédits == 0', async () => {
    const context = { auth: { uid: 'user-zero' } };
    await expect(decrementCredits({}, context)).rejects.toMatchObject({ code: 'failed-precondition' });
  });

  test('decrementCredits échoue si document utilisateur manquant', async () => {
    const context = { auth: { uid: 'user-missing' } };
    await expect(decrementCredits({}, context)).rejects.toMatchObject({ code: 'not-found' });
  });

  test('decrementCredits échoue si non authentifié', async () => {
    await expect(decrementCredits({}, {})).rejects.toMatchObject({ code: 'unauthenticated' });
  });

  test('getCredits retourne le solde courant', async () => {
    const context = { auth: { uid: 'user-positive' } };
    const res = await getCredits({}, context);
    expect(res).toEqual({ credits: 2 });
  });
});


