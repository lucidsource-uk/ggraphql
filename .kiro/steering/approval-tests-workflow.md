---
inclusion: manual
---

# ApprovalTests Workflow

This project uses ApprovalTests for snapshot testing of generated code. Here's how to work with it:

## Understanding ApprovalTests

- **`.approved.*` files**: These contain the expected/approved output
- **`.received.*` files**: These are generated when tests fail, showing the actual output
- When a test passes, no `.received.*` files are created

## Running Tests

```bash
./gradlew test
```

## When Tests Fail

1. **Check the `.received.*` files** - these show what was actually generated
2. **Compare with `.approved.*` files** - these show what was expected
3. **If the new output is correct**, approve it by replacing the `.approved.*` file with the `.received.*` file

## Auto-Approving Changes

To automatically approve all changes (useful when intentionally updating generated code):

1. **Temporarily modify the test class**:
   ```kotlin
   // Comment out the current reporter
   // @UseReporter(JunitReporter::class)
   
   // Uncomment the auto-approve reporter
   @UseReporter(AutoApproveReporter::class)
   ```

2. **Run the tests** - this will automatically update all `.approved.*` files

3. **Revert the reporter change** back to `JunitReporter::class`

## Custom Approval Namer

The project uses `TestNameApprovalNamer` to organize approval files in subdirectories:

```
__approvals__/
└── testName/
    ├── file1.approved.text
    ├── file2.approved.text
    └── ...
```

This keeps approval files organized by test name, making them easier to manage.

## Best Practices

- **Review changes carefully** before auto-approving
- **Keep approval files in version control** - they document expected behavior
- **Use descriptive test names** since they become directory names
- **Run tests after making changes** to catch regressions early