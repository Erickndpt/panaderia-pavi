interface ProcesoNode {
  getBuiltinModule?: (id: string) => { webcrypto?: Crypto };
}

const proceso = (globalThis as { process?: ProcesoNode }).process;
const webcrypto = proceso?.getBuiltinModule?.('node:crypto')?.webcrypto;

if (!globalThis.crypto?.subtle && webcrypto) {
  Object.defineProperty(globalThis, 'crypto', {
    configurable: true,
    value: webcrypto,
  });
}
