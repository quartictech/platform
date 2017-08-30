import os
from setuptools import setup, find_packages

setup(name="quarty",
      version=os.environ.get("CIRCLE_BUILD_NUM", "unknown"),
      description="Quartic runner",
      author="Quartic Technologies",
      author_email="contact@quartic.io",
      url="https://www.quartic.io",
      packages=find_packages("src"),
      package_dir={"":"src"},
      install_requires=[
          "aiohttp==2.2.5",
          "PyYAML==3.12",
          "requests==2.18.4",
          "datadiff==2.0.0"
      ],
      zip_safe=False)
