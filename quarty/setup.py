import os
from setuptools import setup, find_packages

test_deps = [
    "mock==2.0.0",
    "pytest==3.0.7",
    "pylint==1.7.1",
    "pylint-quotes==0.1.5",
    "pytest-runner==2.11.1",
    "setuptools-lint==0.5.2"
]

setup(name="quarty",
      version=os.environ.get("CIRCLE_BUILD_NUM", "0"),
      description="Quartic runner",
      author="Quartic Technologies",
      author_email="contact@quartic.io",
      url="https://www.quartic.io",
      packages=find_packages("src"),
      package_dir={"":"src"},
      install_requires=[
          "aiohttp==3.7.4",
          "PyYAML==3.12",
          "requests==2.18.4",
          "datadiff==2.0.0",

      ],
      extras_require={
          "test": test_deps,
      },
      tests_require=test_deps
)
